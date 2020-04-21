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

    default:
      break
  }
}
