import Router from 'next/router'

import { fetcher, getQueryString } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { name, type },
}) => {
  dispatch({ isLoading: true })

  try {
    await fetcher(`/api/v1/projects/${projectId}/models/`, {
      method: 'POST',
      body: JSON.stringify({ name, type }),
    })

    const queryString = getQueryString({
      action: 'add-model-success',
    })

    Router.push(
      `/[projectId]/models${queryString}`,
      `/${projectId}/models${queryString}`,
    )
  } catch (response) {
    try {
      const errors = await response.json()

      const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
        acc[errorKey] = errors[errorKey].join(' ')
        return acc
      }, {})

      dispatch({ isLoading: false, errors: parsedErrors })

      window.scrollTo(0, 0)
    } catch (error) {
      dispatch({
        isLoading: false,
        errors: { global: 'Something went wrong. Please try again.' },
      })
    }
  }
}
