import { useReducer } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'

import { spacing } from '../Styles'

import Form from '../Form'
import FlashMessageErrors from '../FlashMessage/Errors'
import SectionTitle from '../SectionTitle'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Textarea, { VARIANTS as TEXTAREA_VARIANTS } from '../Textarea'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onSubmit } from './helpers'

const reducer = (state, action) => ({ ...state, ...action })

const ModelsEdit = ({ projectId, model }) => {
  const INITIAL_STATE = {
    name: model.name,
    description: model.description,
    isLoading: false,
    errors: {},
  }

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  return (
    <Form style={{ padding: 0 }}>
      <FlashMessageErrors
        errors={state.errors}
        styles={{ paddingTop: spacing.normal }}
      />

      <SectionTitle>Model Name &amp; Description</SectionTitle>

      <Input
        autoFocus
        id="name"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Model Name:"
        type="text"
        value={state.name}
        onChange={({ target: { value } }) => {
          dispatch({ name: value })
        }}
        hasError={state.errors.name !== undefined}
        errorMessage={state.errors.name}
      />

      <Textarea
        id="description"
        variant={TEXTAREA_VARIANTS.SECONDARY}
        label="Description (optional):"
        value={state.description}
        onChange={({ target: { value } }) => {
          dispatch({ description: value })
        }}
        hasError={state.errors.description !== undefined}
        errorMessage={state.errors.description}
      />

      <ButtonGroup>
        <Link href={`/${projectId}/models/${model.id}`} passHref>
          <Button variant={BUTTON_VARIANTS.SECONDARY}>Cancel</Button>
        </Link>

        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ projectId, model, state, dispatch })}
          isDisabled={!state.name || state.isLoading}
        >
          {state.isLoading ? 'Saving...' : 'Save Model'}
        </Button>
      </ButtonGroup>
    </Form>
  )
}

ModelsEdit.propTypes = {
  projectId: PropTypes.string.isRequired,
  model: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
  }).isRequired,
}

export default ModelsEdit
