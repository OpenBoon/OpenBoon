import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR, { useSWRPages } from 'swr'
import AutoSizer from 'react-virtualized-auto-sizer'
import { FixedSizeGrid } from 'react-window'

import Loading from '../Loading'

import AssetsResize from './Resize'
import AssetsThumbnail from './Thumbnail'
import AssetsLoadMore from './LoadMore'

import { reducer, INITIAL_STATE } from './reducer'

const SIZE = 50

const Assets = () => {
  const {
    query: { projectId },
  } = useRouter()

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  const { thumbnailCount, isMin, isMax } = state

  const {
    pages,
    pageCount,
    isLoadingMore,
    isReachingEnd,
    loadMore,
    pageSWRs,
  } = useSWRPages(
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
        /* istanbul ignore next */
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
    /* istanbul ignore next */
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

  return (
    <div css={{ flex: 1, position: 'relative', overflow: 'scroll' }}>
      {pages}
      <AutoSizer>
        {({ height, width }) => (
          <FixedSizeGrid
            columnCount={thumbnailCount}
            columnWidth={Math.max(100, width / thumbnailCount)}
            rowHeight={Math.max(100, width / thumbnailCount)}
            rowCount={Math.ceil(items.length / thumbnailCount)}
            width={width}
            height={height}
          >
            {({ columnIndex, rowIndex, style }) => {
              const index = columnIndex + rowIndex * thumbnailCount
              if (!items[index]) return null
              return (
                <div style={style}>
                  <AssetsThumbnail asset={items[index]} />
                </div>
              )
            }}
          </FixedSizeGrid>
        )}
      </AutoSizer>
      {/* <AssetsLoadMore
        pageCount={pageCount}
        isLoadingMore={isLoadingMore}
        isReachingEnd={isReachingEnd}
        loadMore={loadMore}
      /> */}
      <AssetsResize dispatch={dispatch} isMin={isMin} isMax={isMax} />
    </div>
  )
}

export default Assets
