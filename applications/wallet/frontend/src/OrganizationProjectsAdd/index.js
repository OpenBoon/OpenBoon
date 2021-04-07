import { useReducer } from 'react'
import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import FlashMessageErrors from '../FlashMessage/Errors'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onSubmit } from './helpers'

const INITIAL_STATE = {
  name: '',
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const OrganizationProjectsAdd = () => {
  const {
    query: { organizationId },
  } = useRouter()

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  return (
    <>
      <FlashMessageErrors
        errors={state.errors}
        styles={{ paddingTop: spacing.base, paddingBottom: spacing.base }}
      />

      <SectionTitle>Create a New Project</SectionTitle>

      <Form>
        <Input
          autoFocus
          id="name"
          variant={INPUT_VARIANTS.SECONDARY}
          label="Project Name"
          type="text"
          value={state.name}
          onChange={({ target: { value } }) => dispatch({ name: value })}
          hasError={state.errors.name !== undefined}
          errorMessage={state.errors.name}
        />

        <ButtonGroup>
          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() => onSubmit({ organizationId, dispatch, state })}
            isDisabled={!state.name || state.isLoading}
          >
            {state.isLoading ? 'Creating...' : 'Create Project'}
          </Button>
        </ButtonGroup>
      </Form>
    </>
  )
}

export default OrganizationProjectsAdd
