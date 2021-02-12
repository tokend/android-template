package io.tokend.template.features.assets.storage

import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.tokend.template.data.storage.repository.MultipleItemsRepository
import io.tokend.template.data.storage.repository.RepositoryCache
import io.tokend.template.extensions.mapSuccessful
import io.tokend.template.features.assets.model.AssetRecord
import io.tokend.template.features.urlconfig.providers.UrlConfigProvider
import io.tokend.template.logic.providers.ApiProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.assets.params.AssetsPageParams
import org.tokend.sdk.utils.SimplePagedResourceLoader

class AssetsRepository(
    private val ownerId: String?,
    private val apiProvider: ApiProvider,
    private val urlConfigProvider: UrlConfigProvider,
    private val mapper: ObjectMapper,
    itemsCache: RepositoryCache<AssetRecord>
) : MultipleItemsRepository<AssetRecord>(itemsCache) {
    private val mItemsMap = mutableMapOf<String, AssetRecord>()
    val itemsMap: Map<String, AssetRecord> = mItemsMap

    override fun getItems(): Single<List<AssetRecord>> {

        val loader = SimplePagedResourceLoader(
            { nextCursor ->
                apiProvider.getApi().v3.assets.get(
                    AssetsPageParams(
                        owner = ownerId,
                        pagingParams = PagingParamsV2(
                            page = nextCursor,
                            limit = PAGE_LIMIT
                        )
                    )
                )
            }
        )

        return loader
            .loadAll()
            .toSingle()
            .map { assetResources ->
                assetResources.mapSuccessful {
                    AssetRecord.fromResource(it, urlConfigProvider.getConfig(), mapper)
                }
            }
    }

    /**
     * @return single asset info
     */
    fun getSingle(code: String): Single<AssetRecord> {
        return itemsCache
            .loadFromDb()
            .toSingle { itemsCache.items }
            .flatMapMaybe { cachedItems ->
                cachedItems.find { it.code == code }.toMaybe()
            }
            .switchIfEmpty(
                apiProvider.getApi()
                    .v3
                    .assets
                    .getById(code)
                    .toSingle()
                    .map { AssetRecord.fromResource(it, urlConfigProvider.getConfig(), mapper) }
                    .doOnSuccess {
                        itemsCache.updateOrAdd(it)
                    }
            )
    }

    /**
     * Ensures that given assets are loaded
     */
    fun ensureAssets(codes: Collection<String>): Single<Map<String, AssetRecord>> {
        return if (itemsMap.keys.containsAll(codes))
            Single.just(itemsMap)
        else
            updateDeferred()
                .toSingle { itemsMap }
    }

    override fun broadcast() {
        mItemsMap.clear()
        itemsCache.items.associateByTo(mItemsMap, AssetRecord::code)
        super.broadcast()
    }

    private companion object {
        private const val PAGE_LIMIT = 20
    }
}