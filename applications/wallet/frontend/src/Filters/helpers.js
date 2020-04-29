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

export const decode = ({ query }) => {
  try {
    return JSON.parse(atob(query))
  } catch (error) {
    return []
  }
}

export const cleanup = ({ query }) => {
  const filters = decode({ query }).filter(
    (filter) => Object.keys(filter.values).length > 0,
  )

  return encode({ filters })
}

export const dispatch = ({ action, payload }) => {
  switch (action) {
    case ACTIONS.ADD_FILTERS: {
      const { projectId, assetId, filters, newFilters } = payload

      const query = encode({ filters: [...filters, ...newFilters] })

      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, id: assetId, query },
        },
        `/${projectId}/visualizer${formatUrl({ id: assetId, query })}`,
      )

      break
    }

    case ACTIONS.UPDATE_FILTER: {
      const {
        projectId,
        assetId,
        filters,
        updatedFilter,
        filterIndex,
      } = payload

      const query = encode({
        filters: [
          ...filters.slice(0, filterIndex),
          updatedFilter,
          ...filters.slice(filterIndex + 1),
        ],
      })

      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, id: assetId, query },
        },
        `/${projectId}/visualizer${formatUrl({ id: assetId, query })}`,
      )

      break
    }

    case ACTIONS.DELETE_FILTER: {
      const { projectId, assetId, filters, filterIndex } = payload

      const newFilters = [
        ...filters.slice(0, filterIndex),
        ...filters.slice(filterIndex + 1),
      ]

      const query = newFilters.length > 0 ? encode({ filters: newFilters }) : ''

      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, id: assetId, query },
        },
        `/${projectId}/visualizer${formatUrl({ id: assetId, query })}`,
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
