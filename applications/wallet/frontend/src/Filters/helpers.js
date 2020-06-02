import Router from 'next/router'
import utf8 from 'utf8'

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
  ADD_OR_REPLACE_FILTER: 'ADD_OR_REPLACE_FILTER',
  DELETE_FILTER: 'DELETE_FILTER',
  CLEAR_FILTERS: 'CLEAR_FILTERS',
}

export const encode = ({ filters }) => {
  return btoa(utf8.encode(JSON.stringify(filters)))
}

export const decode = ({ query }) => {
  try {
    return JSON.parse(utf8.decode(atob(query)))
  } catch (error) {
    return []
  }
}

export const cleanup = ({ query }) => {
  const filters = decode({ query }).filter(
    ({ values = {}, isDisabled = false }) =>
      Object.keys(values).length > 0 && !isDisabled,
  )

  return encode({ filters })
}

export const dispatch = ({ action, payload }) => {
  switch (action) {
    case ACTIONS.ADD_FILTERS: {
      const { projectId, assetId, filters, newFilters } = payload

      const query = encode({ filters: [...newFilters, ...filters] })

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

    case ACTIONS.ADD_OR_REPLACE_FILTER: {
      const { projectId, assetId, filters, newFilter, filterIndex } = payload
      let query
      if (filterIndex === -1) {
        // if filter does not exist, add
        query = encode({ filters: [newFilter, ...filters] })
      } else {
        // if filter exists, update
        query = encode({
          filters: [
            ...filters.slice(0, filterIndex),
            newFilter,
            ...filters.slice(filterIndex + 1),
          ],
        })
      }

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
