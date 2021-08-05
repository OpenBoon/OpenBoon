import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'
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

const DatasetsEditForm = () => {
  const {
    query: { projectId, datasetId },
  } = useRouter()

  const { data: dataset } = useSWR(
    `/api/v1/projects/${projectId}/datasets/${datasetId}/`,
  )

  const INITIAL_STATE = {
    name: dataset.name,
    description: dataset.description,
    type: dataset.type,
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

      <SectionTitle>Dataset Name &amp; Description</SectionTitle>

      <Input
        autoFocus
        id="name"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Dataset Name:"
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
        <Link href={`/${projectId}/datasets/${datasetId}`} passHref>
          <Button variant={BUTTON_VARIANTS.SECONDARY}>Cancel</Button>
        </Link>

        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ projectId, datasetId, state, dispatch })}
          isDisabled={!state.name || state.isLoading}
        >
          {state.isLoading ? 'Saving...' : 'Save Dataset'}
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default DatasetsEditForm
