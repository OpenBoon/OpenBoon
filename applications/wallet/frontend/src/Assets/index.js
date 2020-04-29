import { useReducer, useRef, forwardRef } from 'react'
import { useRouter } from 'next/router'
import useSWR, { useSWRPages } from 'swr'
import AutoSizer from 'react-virtualized-auto-sizer'
import InfiniteLoader from 'react-window-infinite-loader'
import { FixedSizeGrid } from 'react-window'

import Loading from '../Loading'

import AssetsResize from './Resize'
import AssetsThumbnail from './Thumbnail'

import { reducer, INITIAL_STATE } from './reducer'
import { spacing, constants } from '../Styles'

const SIZE = 100
const PADDING_SIZE = spacing.small

/* istanbul ignore next */
const Assets = () => {
  const {
    query: { projectId, query = 'W10=' },
  } = useRouter()

  const innerRef = useRef()

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  const { columnCount, isMin, isMax } = state

  const { pages, loadMore, pageSWRs } = useSWRPages(
    // page key
    'visualizer',

    // page component
    ({ offset, withSWR }) => {
      const from = offset * SIZE
      const { data: { results } = {} } = withSWR(
        // eslint-disable-next-line react-hooks/rules-of-hooks
        useSWR(
          `/api/v1/projects/${projectId}/searches/query/?query=${query}&from=${from}&size=${SIZE}`,
          { suspense: false },
        ),
      )

      if (!results) {
        if (offset > 0) return null

        return (
          <div css={{ flex: 1, display: 'flex', height: '100%' }}>
            <Loading />
          </div>
        )
      }

      return null
    },

    // offset of next page
    ({ data: { count } }, index) => {
      const offset = (index + 1) * SIZE
      return offset < count ? index + 1 : null
    },

    // deps of the page component
    [query],
  )

  const items = Array.isArray(pageSWRs)
    ? pageSWRs.flatMap((pageSWR) => {
        const { data: { results } = {} } = pageSWR || {}
        return results
      })
    : []

  const { data: { count: itemCount } = {} } = pageSWRs[0] || {}

  return (
    <div
      css={{
        flex: 1,
        position: 'relative',
        marginBottom: -spacing.mini,
        boxShadow: constants.boxShadows.assets,
      }}
    >
      {pages}
      <AutoSizer>
        {({ height, width }) => (
          <InfiniteLoader
            isItemLoaded={(index) => !!items[index]}
            itemCount={itemCount}
            loadMoreItems={loadMore}
          >
            {({ onItemsRendered, ref }) => {
              const { parentElement } = (innerRef && innerRef.current) || {}
              const { offsetWidth = 0, clientWidth = 0 } = parentElement || {}
              const adjustedWidth = width - PADDING_SIZE * 2
              const scrollbarSize = offsetWidth - clientWidth
              const thumbnailSize = Math.max(100, adjustedWidth / columnCount)
              const rowCount = Math.ceil(items.length / columnCount)
              const hasVerticalScrollbar = rowCount * thumbnailSize > height
              const scrollbarBuffer = hasVerticalScrollbar ? scrollbarSize : 0
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
                      visibleRowStopIndex * columnCount + visibleColumnStopIndex

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

                    if (!items[index]) return null

                    return (
                      <div
                        style={{
                          ...style,
                          top: `${parseFloat(style.top) + PADDING_SIZE}px`,
                          left: `${parseFloat(style.left) + PADDING_SIZE}px`,
                        }}
                      >
                        <AssetsThumbnail asset={items[index]} />
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
    </div>
  )
}

export default Assets
