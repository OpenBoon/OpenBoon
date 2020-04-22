import { useRouter } from 'next/router'

import { colors, spacing } from '../Styles'

import { dispatch, ACTIONS } from './helpers'

import SearchFilter from '../SearchFilter'

const Filters = () => {
  const {
    query: { projectId, id: assetId = '', filters: f = '[]' },
  } = useRouter()

  const filters = JSON.parse(f || '[]')

  return (
    <div
      css={{
        flex: 1,
        backgroundColor: colors.structure.lead,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        padding: spacing.small,
      }}
    >
      <SearchFilter projectId={projectId} assetId={assetId} filters={filters} />
      <ul css={{ padding: 0 }}>
        {filters.map((filter, index) => (
          <li
            // eslint-disable-next-line react/no-array-index-key
            key={`${filter.type}-${index}`}
            css={{
              display: 'flex',
              justifyContent: 'space-between',
            }}
          >
            • {filter.value}
            <button
              type="button"
              onClick={() =>
                dispatch({
                  action: ACTIONS.DELETE_FILTER,
                  payload: {
                    projectId,
                    assetId,
                    filters,
                    filterIndex: index,
                  },
                })
              }
            >
              delete
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default Filters
