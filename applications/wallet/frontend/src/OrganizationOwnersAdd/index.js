import { useReducer } from 'react'
import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import FlashMessageErrors from '../FlashMessage/Errors'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import ProjectUsersAddCopyLink from '../ProjectUsersAdd/CopyLink'

import { onSubmit } from './helpers'

import OrganizationOwnersAddResponse from './Response'

const INITIAL_STATE = {
  emails: '',
  succeeded: [],
  failed: [],
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const OrganizationOwnersAdd = () => {
  const {
    query: { organizationId },
  } = useRouter()

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  if (state.succeeded.length > 0 || state.failed.length > 0) {
    return (
      <OrganizationOwnersAddResponse
        organizationId={organizationId}
        succeeded={state.succeeded}
        failed={state.failed}
        roles={state.roles}
        onReset={() => dispatch(INITIAL_STATE)}
      />
    )
  }

  return (
    <>
      <FlashMessageErrors
        errors={state.errors}
        styles={{ paddingTop: spacing.base, paddingBottom: spacing.base }}
      />

      <SectionTitle>Add Owners(s) to Organization</SectionTitle>

      <ProjectUsersAddCopyLink />

      <Form>
        <Input
          autoFocus
          id="emails"
          variant={INPUT_VARIANTS.SECONDARY}
          label="Email(s)"
          type="text"
          value={state.emails}
          onChange={({ target: { value } }) => dispatch({ emails: value })}
          hasError={state.errors.emails !== undefined}
          errorMessage={state.errors.emails}
        />

        <ButtonGroup>
          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() => onSubmit({ organizationId, dispatch, state })}
            isDisabled={!state.emails || state.isLoading}
          >
            {state.isLoading ? 'Adding...' : 'Add'}
          </Button>
        </ButtonGroup>
      </Form>
    </>
  )
}

export default OrganizationOwnersAdd
