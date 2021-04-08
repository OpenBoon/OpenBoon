import Router from 'next/router'

import {
  fetcher,
  revalidate,
  getQueryString,
  parseResponse,
} from '../Fetch/helpers'

export const onSubmit = async ({
  organizationId,
  dispatch,
  state: { name },
}) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    await fetcher(`/api/v1/organizations/${organizationId}/projects/`, {
      method: 'POST',
      body: JSON.stringify({ name }),
    })

    await revalidate({
      key: `/api/v1/organizations/${organizationId}/projects/`,
    })

    const queryString = getQueryString({ action: 'add-project-success' })

    Router.push(
      `/organizations/[organizationId]${queryString}`,
      `/organizations/${organizationId}`,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
