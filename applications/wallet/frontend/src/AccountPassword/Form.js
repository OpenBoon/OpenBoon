import { useRouter } from 'next/router'
import { useState, useReducer } from 'react'

import { spacing } from '../Styles'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { onSubmit } from './helpers'

const reducer = (state, action) => ({ ...state, ...action })

const AccountPasswordForm = () => {
  const {
    query: { projectId },
  } = useRouter()

  const [errors, setErrors] = useState({})
  const [state, dispatch] = useReducer(reducer, {
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })

  return (
    <Form>
      <Input
        autoFocus
        id="currentPassword"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Current Password"
        type="password"
        value={state.currentPassword}
        onChange={({ target: { value } }) =>
          dispatch({ currentPassword: value })
        }
        hasError={errors.oldPassword !== undefined}
        errorMessage={errors.oldPassword}
      />

      <Input
        id="newPassword"
        variant={INPUT_VARIANTS.SECONDARY}
        label="New Password"
        type="password"
        value={state.newPassword}
        onChange={({ target: { value } }) => dispatch({ newPassword: value })}
        hasError={errors.newPassword1 !== undefined}
        errorMessage={errors.newPassword1}
      />

      <Input
        id="confirmPassword"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Confirm Password"
        type="password"
        value={state.confirmPassword}
        onChange={({ target: { value } }) =>
          dispatch({ confirmPassword: value })
        }
        hasError={errors.newPassword2 !== undefined}
        errorMessage={errors.newPassword2}
      />

      <div
        css={{
          paddingTop: spacing.moderate,
          paddingBottom: spacing.moderate,
        }}>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ dispatch, setErrors, projectId, ...state })}
          isDisabled={
            !state.currentPassword ||
            !state.newPassword ||
            !state.confirmPassword
          }>
          Save
        </Button>
      </div>
    </Form>
  )
}

export default AccountPasswordForm
