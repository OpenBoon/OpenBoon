import { useReducer } from 'react'
import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import Form from '../Form'
import Input from '../Input'
import Button, { VARIANTS } from '../Button'

const reducer = (state, action) => ({ ...state, ...action })

const AccountPasswordForm = ({ onSubmit }) => {
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
        label="Current Password"
        type="password"
        value={state.currentPassword}
        onChange={({ target: { value } }) =>
          dispatch({ currentPassword: value })
        }
        hasError={false}
      />

      <Input
        id="newPassword"
        label="New Password"
        type="password"
        value={state.newPassword}
        onChange={({ target: { value } }) => dispatch({ newPassword: value })}
        hasError={false}
      />

      <Input
        id="confirmPassword"
        label="Confirm Password"
        type="password"
        value={state.confirmPassword}
        onChange={({ target: { value } }) =>
          dispatch({ confirmPassword: value })
        }
        hasError={false}
      />

      <div
        css={{
          paddingTop: spacing.moderate,
          paddingBottom: spacing.moderate,
        }}>
        <Button
          type="submit"
          variant={VARIANTS.PRIMARY}
          onClick={() => onSubmit(state)}
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

AccountPasswordForm.propTypes = {
  onSubmit: PropTypes.func.isRequired,
}

export default AccountPasswordForm
