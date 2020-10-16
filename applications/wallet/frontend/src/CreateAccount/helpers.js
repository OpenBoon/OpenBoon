import Router from 'next/router'

import { getCsrfToken } from '../Fetch/helpers'
import { CURRENT_POLICIES_DATE } from '../Policies/helpers'

const BASE_HEADER = {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    'X-CSRFToken': getCsrfToken(),
  },
}

export const onRegister = async ({
  dispatch,
  state: { firstName, lastName, email, password },
}) => {
  dispatch({ isLoading: true })

  try {
    const response = await fetch(`/api/v1/accounts/register`, {
      ...BASE_HEADER,
      body: JSON.stringify({
        email,
        firstName,
        lastName,
        password,
        policiesDate: CURRENT_POLICIES_DATE,
      }),
    })

    if (response.status >= 400) throw response

    Router.push('/create-account-success', '/')
  } catch (response) {
    dispatch({
      isLoading: false,
      error: 'Something went wrong. Please try again.',
    })
  }
}

export const onConfirm = async ({ uid, token }) => {
  try {
    const response = await fetch(`/api/v1/accounts/confirm`, {
      ...BASE_HEADER,
      body: JSON.stringify({
        userId: uid,
        token,
      }),
    })

    if (response.status >= 400) throw response

    Router.push('/?action=account-activation-success', '/')
  } catch (response) {
    Router.push(
      '/create-account?action=account-activation-expired',
      '/create-account',
    )
  }
}
