import { useReducer } from 'react'
import PropTypes from 'prop-types'

import { getUser } from '../Authentication/helpers'

import { spacing, typography } from '../Styles'

import FormAlert from '../FormAlert'
import SectionTitle from '../SectionTitle'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'

import { onConfirm } from './helpers'

export const noop = () => () => {}

const INITIAL_STATE = {
  newPassword: '',
  confirmPassword: '',
  errors: { submit: '', confirm: '' },
}

const reducer = (state, action) => ({ ...state, ...action })

const ResetPasswordConfirm = ({ uid, token }) => {
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  const { email } = getUser()

  const mismatchingEmails = state.confirmPassword
    ? state.newPassword !== state.confirmPassword
    : false

  return (
    <>
      <FormAlert
        errorMessage={state.errors.submit}
        setErrorMessage={() => dispatch({ errors: { submit: '' } })}
      />

      <h3
        css={{
          textAlign: 'center',
          fontSize: typography.size.large,
          lineHeight: typography.height.large,
          fontWeight: typography.weight.regular,
          paddingTop: spacing.spacious,
          paddingBottom: spacing.spacious,
        }}>
        Enter New Password
      </h3>

      <Input
        id="newPassword"
        variant={INPUT_VARIANTS.PRIMARY}
        label="Password"
        type="password"
        value={state.newPassword}
        onChange={({ target: { value } }) => dispatch({ newPassword: value })}
        hasError={false}
        errorMessage=""
      />
      <Input
        id="confirmPassword"
        variant={INPUT_VARIANTS.PRIMARY}
        label="Confirm Password"
        type="password"
        value={state.confirmPassword}
        onChange={({ target: { value } }) =>
          dispatch({ confirmPassword: value })
        }
        hasError={mismatchingEmails}
        errorMessage={state.errors.confirm}
        onBlur={() => {
          if (state.newPassword !== state.confirmPassword) {
            dispatch({ errors: { confirm: 'Passwords do not match' } })
          }
          if (state.newPassword === state.confirmPassword) {
            dispatch({ errors: { confirm: '' } })
          }
        }}
      />

      <div
        css={{
          paddingTop: spacing.normal,
          display: 'flex',
          justifyContent: 'center',
        }}>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() =>
            onConfirm({
              state,
              dispatch,
              uid,
              token,
            })
          }
          isDisabled={
            !state.newPassword ||
            !state.confirmPassword ||
            state.newPassword !== state.confirmPassword
          }>
          Save
        </Button>
      </div>
    </>
  )
}

ResetPasswordConfirm.propTypes = {
  uid: PropTypes.string.isRequired,
  token: PropTypes.string.isRequired,
}

export default ResetPasswordConfirm
