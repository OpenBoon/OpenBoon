import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR, { useSWRPages } from 'swr'
import AutoSizer from 'react-virtualized-auto-sizer'
import InfiniteLoader from 'react-window-infinite-loader'
import { FixedSizeGrid } from 'react-window'

import Loading from '../Loading'

import AssetsResize from './Resize'
import AssetsThumbnail from './Thumbnail'

import { reducer, INITIAL_STATE } from './reducer'

const SIZE = 100

/* istanbul ignore next */
const Assets = () => {
  const {
    query: { projectId },
  } = useRouter()

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
          `/api/v1/projects/${projectId}/assets/?from=${from}&size=${SIZE}`,
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
    [],
  )

  const items = Array.isArray(pageSWRs)
    ? pageSWRs.flatMap((pageSWR) => {
        const { data: { results } = {} } = pageSWR || {}
        return results
      })
    : []

  const { data: { count: itemCount } = {} } = pageSWRs[0] || {}

  return (
    <div css={{ flex: 1, position: 'relative', overflow: 'scroll' }}>
      {pages}
      <AutoSizer>
        {({ height, width }) => (
          <InfiniteLoader
            isItemLoaded={(index) => !!items[index]}
            itemCount={itemCount}
            loadMoreItems={loadMore}
          >
            {({ onItemsRendered, ref }) => (
              <FixedSizeGrid
                ref={ref}
                onItemsRendered={({
                  visibleRowStartIndex,
                  visibleRowStopIndex,
                  visibleColumnStartIndex,
                  visibleColumnStopIndex,
                }) => {
                  const visibleStartIndex =
                    visibleRowStartIndex * columnCount + visibleColumnStartIndex

                  const visibleStopIndex =
                    visibleRowStopIndex * columnCount + visibleColumnStopIndex

                  onItemsRendered({
                    visibleStartIndex,
                    visibleStopIndex,
                  })
                }}
                columnCount={columnCount}
                columnWidth={Math.max(100, width / columnCount)}
                rowHeight={Math.max(100, width / columnCount)}
                rowCount={Math.ceil(items.length / columnCount)}
                width={width}
                height={height}
              >
                {({ columnIndex, rowIndex, style }) => {
                  const index = columnIndex + rowIndex * columnCount

                  if (!items[index]) return null

                  return (
                    <div style={style}>
                      <AssetsThumbnail asset={items[index]} />
                    </div>
                  )
                }}
              </FixedSizeGrid>
            )}
          </InfiniteLoader>
        )}
      </AutoSizer>
      <AssetsResize dispatch={dispatch} isMin={isMin} isMax={isMax} />
    </div>
  )
}

export default Assets
