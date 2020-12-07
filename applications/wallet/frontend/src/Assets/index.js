/* eslint-disable react/prop-types */
import { useRef, forwardRef, useEffect, useState } from 'react'
import { useRouter } from 'next/router'
import { useSWRInfinite } from 'swr'
import AutoSizer from 'react-virtualized-auto-sizer'
import InfiniteLoader from 'react-window-infinite-loader'
import { FixedSizeGrid } from 'react-window'

import { constants, spacing } from '../Styles'

import { cleanup } from '../Filters/helpers'
import { useLocalStorage } from '../LocalStorage/helpers'

import Loading from '../Loading'
import VisualizerNavigation from '../Visualizer/Navigation'

import { reducer, INITIAL_STATE } from './reducer'

import AssetsResize from './Resize'
import AssetsThumbnail from './Thumbnail'
import AssetsEmpty from './Empty'
import AssetsQuickView from './QuickView'

const SIZE = 100
const PADDING_SIZE = spacing.small

/* istanbul ignore next */
const Assets = () => {
  const {
    pathname,
    query: { projectId, assetId, query },
  } = useRouter()

  const innerRef = useRef()
  const [virtualLoaderRef, setVirtualLoaderRef] = useState()

  const [state, dispatch] = useLocalStorage({
    key: 'Assets',
    reducer,
    initialState: INITIAL_STATE,
  })

  const { columnCount, isMin, isMax } = state

  const q = cleanup({ query })

  const { data, size, setSize } = useSWRInfinite(
    (pageIndex, previousPageData) => {
      if (previousPageData && !previousPageData.next) return null

      const from = pageIndex * SIZE

      return `/api/v1/projects/${projectId}/searches/query/?query=${q}&from=${from}&size=${SIZE}`
    },
    undefined,
    {
      suspense: false,
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      shouldRetryOnError: false,
    },
  )

  const { count: itemCount } = (data && data[0]) || {}

  const assets = Array.isArray(data)
    ? data
        .flatMap((page) => {
          const { results } = page || {}
          return results
        })
        .filter((a) => a && a.id)
    : []

  const selectedRow =
    assets.length && assetId
      ? Math.floor(
          assets.findIndex((item) => item && item.id === assetId) / columnCount,
        )
      : ''

  useEffect(() => {
    if (
      selectedRow &&
      virtualLoaderRef &&
      // eslint-disable-next-line no-underscore-dangle
      virtualLoaderRef._listRef
    ) {
      // eslint-disable-next-line no-underscore-dangle
      virtualLoaderRef._listRef.scrollToItem({
        align: 'smart',
        rowIndex: selectedRow,
      })
    }
  }, [selectedRow, columnCount, assetId, virtualLoaderRef])

  if (!data) {
    return (
      <div css={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <VisualizerNavigation />

        <div css={{ flex: 1, margin: spacing.base, marginBottom: 0 }}>
          <Loading />
        </div>
      </div>
    )
  }

  return (
    <div css={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <VisualizerNavigation />

      <div
        css={{
          flex: 1,
          position: 'relative',
          marginBottom: -spacing.mini,
          boxShadow: constants.boxShadows.inset,
        }}
      >
        <AssetsQuickView assets={assets} columnCount={columnCount} />

        {itemCount === 0 && (
          <AssetsEmpty
            pathname={pathname}
            projectId={projectId}
            assetId={assetId}
            query={query}
          />
        )}

        {!!itemCount && (
          <>
            <AutoSizer>
              {({ height, width }) => (
                <InfiniteLoader
                  ref={(ref) => setVirtualLoaderRef(ref)}
                  isItemLoaded={(index) => !!assets[index]}
                  itemCount={itemCount}
                  loadMoreItems={() => setSize(size + 1)}
                >
                  {({ onItemsRendered, ref }) => {
                    const { parentElement } =
                      (innerRef && innerRef.current) || {}
                    const { offsetWidth = 0, clientWidth = 0 } =
                      parentElement || {}
                    const adjustedWidth = width - PADDING_SIZE * 2
                    const scrollbarSize = offsetWidth - clientWidth
                    const thumbnailSize = Math.max(
                      100,
                      adjustedWidth / columnCount,
                    )
                    const rowCount = Math.ceil(assets.length / columnCount)
                    const hasVerticalScrollbar =
                      rowCount * thumbnailSize > height
                    const scrollbarBuffer = hasVerticalScrollbar
                      ? scrollbarSize
                      : 0
                    const adjustedThumbnailSize = Math.max(
                      100,
                      (adjustedWidth - scrollbarBuffer) / columnCount,
                    )
                    return (
                      <FixedSizeGrid
                        innerRef={innerRef}
                        ref={ref}
                        onItemsRendered={({
                          visibleRowStartIndex,
                          visibleRowStopIndex,
                          visibleColumnStartIndex,
                          visibleColumnStopIndex,
                        }) => {
                          const visibleStartIndex =
                            visibleRowStartIndex * columnCount +
                            visibleColumnStartIndex

                          const visibleStopIndex =
                            visibleRowStopIndex * columnCount +
                            visibleColumnStopIndex

                          onItemsRendered({
                            visibleStartIndex,
                            visibleStopIndex,
                          })
                        }}
                        columnCount={columnCount}
                        columnWidth={adjustedThumbnailSize}
                        rowHeight={adjustedThumbnailSize}
                        rowCount={rowCount}
                        width={width}
                        height={height - PADDING_SIZE / 2}
                        innerElementType={forwardRef(
                          ({ style, ...rest }, elementRef) => (
                            <div
                              ref={elementRef}
                              style={{
                                ...style,
                                width: `${
                                  parseFloat(style.width) + PADDING_SIZE * 2
                                }px`,
                                height: `${
                                  parseFloat(style.height) + PADDING_SIZE * 2
                                }px`,
                              }}
                              // eslint-disable-next-line react/jsx-props-no-spreading
                              {...rest}
                            />
                          ),
                        )}
                      >
                        {({ columnIndex, rowIndex, style }) => {
                          const index = columnIndex + rowIndex * columnCount

                          if (!assets[index]) return null

                          return (
                            <div
                              style={{
                                ...style,
                                top: `${
                                  parseFloat(style.top) + PADDING_SIZE
                                }px`,
                                left: `${
                                  parseFloat(style.left) + PADDING_SIZE
                                }px`,
                              }}
                            >
                              <AssetsThumbnail asset={assets[index]} isActive />
                            </div>
                          )
                        }}
                      </FixedSizeGrid>
                    )
                  }}
                </InfiniteLoader>
              )}
            </AutoSizer>
            <AssetsResize dispatch={dispatch} isMin={isMin} isMax={isMax} />
          </>
        )}
      </div>
    </div>
  )
}

export default Assets
