import { useReducer, useRef } from 'react'
import { useRouter } from 'next/router'
import useSWR, { useSWRPages } from 'swr'
import AutoSizer from 'react-virtualized-auto-sizer'
import InfiniteLoader from 'react-window-infinite-loader'

import { spacing, constants } from '../Styles'

import { cleanup } from '../Filters/helpers'

import Loading from '../Loading'

import { reducer, INITIAL_STATE } from './reducer'

import AssetsResize from './Resize'
import AssetsGrid from './Grid'

const SIZE = 100

/* istanbul ignore next */
const Assets = () => {
  const {
    query: { projectId, id: selectedId, query },
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

      const q = cleanup({ query })

      const { data: { results } = {} } = withSWR(
        // eslint-disable-next-line react-hooks/rules-of-hooks
        useSWR(
          `/api/v1/projects/${projectId}/searches/query/?query=${q}&from=${from}&size=${SIZE}`,
          {
            suspense: false,
            revalidateOnFocus: false,
            revalidateOnReconnect: false,
            shouldRetryOnError: false,
          },
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

  const { data: { count: itemCount } = {} } = pageSWRs[0] || {}

  const items = Array.isArray(pageSWRs)
    ? pageSWRs
        // hack while https://github.com/zeit/swr/issues/189 gets fixed
        .slice(0, Math.ceil(itemCount / SIZE))
        .flatMap((pageSWR) => {
          const { data: { results } = {} } = pageSWR || {}
          return results
        })
    : []

  const selectedRow = items.length
    ? Math.floor(
        items.findIndex((item) => item && item.id === selectedId) / columnCount,
      )
    : ''

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
            {({ onItemsRendered, ref }) => (
              <AssetsGrid
                passedRef={ref}
                innerRef={innerRef}
                height={height}
                width={width}
                items={items}
                columnCount={columnCount}
                onItemsRendered={onItemsRendered}
                selectedRow={selectedRow}
              />
            )}
          </InfiniteLoader>
        )}
      </AutoSizer>
      <AssetsResize dispatch={dispatch} isMin={isMin} isMax={isMax} />
    </div>
  )
}

export default Assets
