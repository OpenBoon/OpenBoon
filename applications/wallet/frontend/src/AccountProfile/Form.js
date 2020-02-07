import { useReducer } from 'react'

import { typography, spacing, colors } from '../Styles'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import FormSuccess from '../FormSuccess'
import SectionTitle from '../SectionTitle'

import { getUser } from '../Authentication/helpers'
import { onSubmit } from './helpers'

const { id, email, firstName = '', lastName = '' } = getUser()

const INITIAL_STATE = {
  id,
  firstName,
  lastName,
  showForm: false,
  success: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const AccountProfileForm = () => {
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  return (
    <>
      {state.success && !state.showForm && (
        <FormSuccess>New Name Saved!</FormSuccess>
      )}

      <SectionTitle>{`User ID: ${email}`}</SectionTitle>

      {!state.showForm && (
        <>
          <div
            css={{
              paddingTop: spacing.comfy,
              fontSize: typography.size.medium,
              lineHeight: typography.height.medium,
              fontWeight: typography.weight.medium,
            }}>
            Username
          </div>

          <div
            css={{
              color: colors.structure.steel,
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
              fontWeight: typography.weight.regular,
              paddingBottom: spacing.comfy,
            }}>
            {`${state.firstName} ${state.lastName}`}
          </div>

          <Button
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() => dispatch({ showForm: true, success: false })}>
            Edit Username
          </Button>
        </>
      )}

      {state.showForm && (
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

          <div css={{ display: 'flex' }}>
            <Button
              css={{ marginRight: spacing.normal }}
              variant={BUTTON_VARIANTS.SECONDARY}
              onClick={() => dispatch(INITIAL_STATE)}>
              Cancel
            </Button>

            <Button
              type="submit"
              variant={BUTTON_VARIANTS.PRIMARY}
              onClick={() => onSubmit({ dispatch, state })}
              isDisabled={!state.firstName || !state.lastName}>
              Save
            </Button>
          </div>
        </Form>
      )}
    </>
  )
}

export default AccountProfileForm
