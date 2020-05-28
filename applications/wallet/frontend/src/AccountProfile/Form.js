import { useContext, useReducer } from 'react'

import { typography, spacing, colors } from '../Styles'

import { UserContext } from '../User'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import SectionTitle from '../SectionTitle'

import { onSubmit } from './helpers'

const INITIAL_STATE = ({ id, firstName, lastName }) => ({
  id,
  firstName,
  lastName,
  showForm: false,
  success: false,
  isLoading: false,
  errors: {},
})

const reducer = (state, action) => ({ ...state, ...action })

const AccountProfileForm = () => {
  const {
    user: { id, email, firstName = '', lastName = '' },
  } = useContext(UserContext)

  const [state, dispatch] = useReducer(
    reducer,
    INITIAL_STATE({ id, firstName, lastName }),
  )

  return (
    <>
      {state.success && !state.showForm && (
        <div
          css={{
            display: 'flex',
            paddingTop: spacing.base,
            paddingBottom: spacing.base,
          }}
        >
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            New name saved.
          </FlashMessage>
        </div>
      )}

      <SectionTitle>{`Email: ${email}`}</SectionTitle>

      {!state.showForm && (
        <>
          <div
            css={{
              paddingTop: spacing.comfy,
              fontSize: typography.size.medium,
              lineHeight: typography.height.medium,
              fontWeight: typography.weight.medium,
            }}
          >
            Name
          </div>

          <div
            css={{
              color: colors.structure.steel,
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
              fontWeight: typography.weight.regular,
              paddingBottom: spacing.comfy,
            }}
          >
            {`${state.firstName} ${state.lastName}`}
          </div>

          <Button
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() => dispatch({ showForm: true, success: false })}
          >
            Edit
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

          <ButtonGroup>
            <Button
              variant={BUTTON_VARIANTS.SECONDARY}
              onClick={() =>
                dispatch(INITIAL_STATE({ id, firstName, lastName }))
              }
            >
              Cancel
            </Button>

            <Button
              type="submit"
              variant={BUTTON_VARIANTS.PRIMARY}
              onClick={() => onSubmit({ dispatch, state })}
              isDisabled={!state.firstName || !state.lastName}
            >
              {state.isLoading ? 'Saving...' : 'Save'}
            </Button>
          </ButtonGroup>
        </Form>
      )}
    </>
  )
}

export default AccountProfileForm
