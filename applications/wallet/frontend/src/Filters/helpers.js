import Router from 'next/router'

export const formatUrl = (params = {}) => {
  const queryString = Object.keys(params)
    .filter((p) => params[p])
    .map((p) => `${p}=${params[p]}`)
    .join('&')

  return queryString ? `?${queryString}` : ''
}

export const ACTIONS = {
  ADD_FILTER: 'ADD_FILTER',
  DELETE_FILTER: 'DELETE_FILTER',
}

export const dispatch = ({ action, payload }) => {
  switch (action) {
    case ACTIONS.ADD_FILTER: {
      const { projectId, assetId, filters: f, filter } = payload

      const filters = JSON.stringify([...f, filter])

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

      const filters = newFilters.length > 0 ? JSON.stringify(newFilters) : ''

      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: { projectId, id: assetId, filters },
        },
        `/${projectId}/visualizer${formatUrl({ id: assetId, filters })}`,
      )

      break
    }

    default:
      break
  }
}
