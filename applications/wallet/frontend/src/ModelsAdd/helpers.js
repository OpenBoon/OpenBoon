import Router from 'next/router'

import { fetcher, revalidate, getQueryString } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { name, type },
}) => {
  dispatch({ isLoading: true })

  try {
    const {
      results: { name: modelName },
    } = await fetcher(`/api/v1/projects/${projectId}/models/`, {
      method: 'POST',
      body: JSON.stringify({ name, type }),
    })

    await revalidate({
      key: `/api/v1/projects/${projectId}/models/`,
      paginated: true,
    })

    const queryString = getQueryString({
      action: 'add-model-success',
      modelName,
    })

    Router.push(`/[projectId]/models${queryString}`, `/${projectId}/models`)
  } catch (response) {
    try {
      const errors = await response.json()

      const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
        acc[errorKey] = errors[errorKey].join(' ')
        return acc
      }, {})

      dispatch({ isLoading: false, errors: parsedErrors })
    } catch (error) {
      dispatch({
        isLoading: false,
        errors: { global: 'Something went wrong. Please try again.' },
      })
    }
  }
}
