import { useReducer } from 'react'

import { spacing } from '../Styles'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { getUser } from '../Authentication/helpers'

import { onSubmit } from './helpers'

const reducer = (state, action) => ({ ...state, ...action })

const AccountProfileForm = () => {
  const { id, firstName = '', lastName = '' } = getUser()

  const [state, dispatch] = useReducer(reducer, {
    id,
    firstName,
    lastName,
    errors: {},
  })

  return (
    <Form>
      <Input
        autoFocus
        id="firstName"
        variant={INPUT_VARIANTS.SECONDARY}
        label="First Name"
        type="text"
        value={state.firstName}
        onChange={({ target: { value } }) => dispatch({ firstName: value })}
        hasError={state.errors.firstName !== undefined}
        errorMessage={state.errors.firstName}
      />

      <Input
        id="lastName"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Last Name"
        type="text"
        value={state.lastName}
        onChange={({ target: { value } }) => dispatch({ lastName: value })}
        hasError={state.errors.lastName !== undefined}
        errorMessage={state.errors.lastName}
      />

      <div
        css={{
          paddingTop: spacing.moderate,
          paddingBottom: spacing.moderate,
        }}>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ dispatch, state })}
          isDisabled={!state.firstName || !state.lastName}>
          Save Changes
        </Button>
      </div>
    </Form>
  )
}

export default AccountProfileForm
