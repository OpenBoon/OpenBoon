import { useReducer } from 'react'

import { spacing } from '../Styles'

import Form from '../Form'
import Input from '../Input'
import Button, { VARIANTS } from '../Button'

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
        label="First Name"
        type="text"
        value={state.firstName}
        onChange={({ target: { value } }) => dispatch({ firstName: value })}
        hasError={false}
      />

      <Input
        id="lastName"
        label="Last Name"
        type="text"
        value={state.lastName}
        onChange={({ target: { value } }) => dispatch({ lastName: value })}
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
          onClick={() => onSubmit({ dispatch, state })}
          isDisabled={!state.firstName || !state.lastName}>
          Save Changes
        </Button>
      </div>
    </Form>
  )
}

export default AccountProfileForm
