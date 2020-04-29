import Router from 'next/router'

export const formatUrl = (params = {}) => {
  const queryString = Object.keys(params)
    .filter((p) => params[p])
    .map((p) => `${p}=${params[p]}`)
    .join('&')

  return queryString ? `?${queryString}` : ''
}

export const ACTIONS = {
  ADD_FILTERS: 'ADD_FILTERS',
  UPDATE_FILTER: 'UPDATE_FILTER',
  DELETE_FILTER: 'DELETE_FILTER',
  CLEAR_FILTERS: 'CLEAR_FILTERS',
}

export const encode = ({ filters }) => {
  return btoa(JSON.stringify(filters))
}

export const decode = ({ filters }) => {
  try {
    return JSON.parse(atob(filters))
  } catch (error) {
    return []
  }
}

export const dispatch = ({ action, payload }) => {
  switch (action) {
    case ACTIONS.ADD_FILTERS: {
      const { projectId, assetId, filters: f, newFilters } = payload

      const filters = encode({ filters: [...f, ...newFilters] })

      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, id: assetId, filters },
        },
        `/${projectId}/visualizer${formatUrl({ id: assetId, filters })}`,
      )

      break
    }

    case ACTIONS.UPDATE_FILTER: {
      const {
        projectId,
        assetId,
        filters: f,
        updatedFilter,
        filterIndex,
      } = payload

      const filters = encode({
        filters: [
          ...f.slice(0, filterIndex),
          updatedFilter,
          ...f.slice(filterIndex + 1),
        ],
      })

      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, id: assetId, filters },
        },
        `/${projectId}/visualizer${formatUrl({ id: assetId, filters })}`,
      )

      break
    }

    case ACTIONS.DELETE_FILTER: {
      const { projectId, assetId, filters: f, filterIndex } = payload

      const newFilters = [
        ...f.slice(0, filterIndex),
        ...f.slice(filterIndex + 1),
      ]

      const filters =
        newFilters.length > 0 ? encode({ filters: newFilters }) : ''

      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, id: assetId, filters },
        },
        `/${projectId}/visualizer${formatUrl({ id: assetId, filters })}`,
      )

      break
    }

    case ACTIONS.CLEAR_FILTERS: {
      const { projectId, assetId } = payload

      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, id: assetId },
        },
        `/${projectId}/visualizer${formatUrl({ id: assetId })}`,
      )

      break
    }

    default:
      break
  }
}
