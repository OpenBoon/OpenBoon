import { useReducer } from 'react'
import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

const reducer = (state, action) => ({ ...state, ...action })

const AccountProfileForm = ({ onSubmit }) => {
  const [state, dispatch] = useReducer(reducer, { firstName: '', lastName: '' })

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
        hasError={false}
      />

      <Input
        id="lastName"
        variant={INPUT_VARIANTS.SECONDARY}
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
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit(state)}
          isDisabled={!state.firstName || !state.lastName}>
          Save Changes
        </Button>
      </div>
    </Form>
  )
}

AccountProfileForm.propTypes = {
  onSubmit: PropTypes.func.isRequired,
}

export default AccountProfileForm
