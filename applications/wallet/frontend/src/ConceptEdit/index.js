import { useReducer } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'

import { spacing } from '../Styles'

import Form from '../Form'
import FlashMessageErrors from '../FlashMessage/Errors'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onSubmit } from './helpers'

const INITIAL_STATE = {
  success: false,
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const ConceptEdit = ({ projectId, datasetId, label }) => {
  const [state, dispatch] = useReducer(reducer, {
    ...INITIAL_STATE,
    label,
    newLabel: label,
  })

  return (
    <Form>
      <FlashMessageErrors
        errors={state.errors}
        styles={{ marginTop: -spacing.base, paddingBottom: spacing.normal }}
      />

      <Input
        autoFocus
        id="newLabel"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Concept"
        type="text"
        value={state.newLabel}
        onChange={({ target: { value } }) => dispatch({ newLabel: value })}
        hasError={state.errors.newLabel !== undefined}
        errorMessage={state.errors.newLabel}
      />

      <ButtonGroup>
        <Link
          href="/[projectId]/datasets/[datasetId]"
          as={`/${projectId}/datasets/${datasetId}`}
          passHref
        >
          <Button variant={BUTTON_VARIANTS.SECONDARY}>Cancel</Button>
        </Link>

        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ projectId, datasetId, dispatch, state })}
          isDisabled={
            !state.label || state.newLabel === label || state.isLoading
          }
        >
          {state.isLoading ? 'Saving...' : 'Save Concept Changes'}
        </Button>
      </ButtonGroup>
    </Form>
  )
}

ConceptEdit.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
}

export default ConceptEdit
