import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import Form from '../Form'
import FlashMessageErrors from '../FlashMessage/Errors'
import SectionTitle from '../SectionTitle'
import SectionSubTitle from '../SectionSubTitle'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Textarea, { VARIANTS as TEXTAREA_VARIANTS } from '../Textarea'
import Radio from '../Radio'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onSubmit } from './helpers'

const INITIAL_STATE = {
  name: '',
  type: '',
  description: '',
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const DatasetsAddForm = () => {
  const {
    query: { projectId },
  } = useRouter()

  const {
    data: { results: datasetTypes },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/dataset_types/`)

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  return (
    <Form>
      <FlashMessageErrors
        errors={state.errors}
        styles={{ marginTop: -spacing.base, paddingBottom: spacing.comfy }}
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

      <SectionTitle>Select a Dataset Type</SectionTitle>

      <SectionSubTitle>
        The type of Dataset determines what kind of Labels to create and which
        Models can be associated with it.
      </SectionSubTitle>

      {datasetTypes.map(({ name, label, description }) => {
        return (
          <div key={name} css={{ paddingTop: spacing.normal }}>
            <Radio
              option={{
                value: name,
                label,
                legend: description,
                initialValue: state.type === name,
              }}
              onClick={({ value }) => dispatch({ type: value })}
            />
          </div>
        )
      })}

      <div css={{ height: spacing.normal }} />

      <ButtonGroup>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ projectId, state, dispatch })}
          isDisabled={!state.name || !state.type || state.isLoading}
        >
          {state.isLoading ? 'Creating...' : 'Create New Dataset'}
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default DatasetsAddForm
