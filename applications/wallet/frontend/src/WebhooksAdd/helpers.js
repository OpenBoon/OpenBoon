import Router from 'next/router'

import {
  fetcher,
  revalidate,
  getQueryString,
  parseResponse,
} from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { url, secretKey, triggers: t, active },
}) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    const triggers = Object.keys(t).filter((key) => t[key])

    await fetcher(`/api/v1/projects/${projectId}/webhooks/`, {
      method: 'POST',
      body: JSON.stringify({ url, secretKey, triggers, active }),
    })

    await revalidate({
      key: `/api/v1/projects/${projectId}/webhooks/`,
    })

    const queryString = getQueryString({
      action: 'add-webhook-success',
    })

    Router.push(`/[projectId]/webhooks${queryString}`, `/${projectId}/webhooks`)
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
