/* eslint-disable no-await-in-loop */
import Router from 'next/router'
import { v4 as uuidv4 } from 'uuid'

import {
  fetcher,
  revalidate,
  getQueryString,
  parseResponse,
} from '../Fetch/helpers'

export const generateSecretKey =
  ({ state, dispatch }) =>
  async () => {
    dispatch({ disableSecretKeyButton: true })

    for (let i = 1; i <= state.secretKey.length; i += 1) {
      dispatch({ secretKey: state.secretKey.slice(i) })
      await new Promise((resolve) => setTimeout(resolve, 0))
    }

    const newSecretKey = uuidv4()

    for (let i = 1; i <= newSecretKey.length; i += 1) {
      dispatch({ secretKey: newSecretKey.slice(-i) })
      await new Promise((resolve) => setTimeout(resolve, 0))
    }

    await navigator.clipboard.writeText(newSecretKey)

    dispatch({ secretKey: newSecretKey })

    dispatch({ disableSecretKeyButton: false, isCopied: true })
  }

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { url, secretKey, triggers: t, active },
}) => {
  dispatch({ isLoading: true, testSent: '', errors: {} })

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

export const onTest = async ({
  dispatch,
  trigger,
  state: { url, secretKey },
}) => {
  dispatch({ testSent: '', errors: {} })

  try {
    await fetcher(`/api/v1/webhooks/test/`, {
      method: 'POST',
      body: JSON.stringify({ url, secretKey, triggers: [trigger.name] }),
    })

    dispatch({ testSent: trigger.displayName, errors: {} })
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ errors })
  }
}
