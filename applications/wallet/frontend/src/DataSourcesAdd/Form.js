import { useReducer } from 'react'
import { useRouter } from 'next/router'
import Link from 'next/link'

import Form, { MAX_WIDTH } from '../Form'
import SectionTitle from '../SectionTitle'
import SectionSubTitle from '../SectionSubTitle'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Textarea, { VARIANTS as TEXTAREA_VARIANTS } from '../Textarea'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import DataSourcesAddAutomaticAnalysis from './AutomaticAnalysis'

const INITIAL_STATE = {
  name: '',
  url: '',
  key: '',
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const DataSourcesAddForm = () => {
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  const {
    query: { projectId },
  } = useRouter()

  return (
    <Form style={{ width: 'auto' }}>
      <div css={{ width: MAX_WIDTH }}>
        <SectionTitle>Data Source Name</SectionTitle>

        <Input
          autoFocus
          id="name"
          variant={INPUT_VARIANTS.SECONDARY}
          label="Name"
          type="text"
          value={state.name}
          onChange={({ target: { value } }) => dispatch({ name: value })}
          hasError={state.errors.name !== undefined}
          errorMessage={state.errors.name}
        />

        <SectionTitle>Connect to Source: Google Cloud Storage</SectionTitle>

        <Input
          id="url"
          variant={INPUT_VARIANTS.SECONDARY}
          label="Bucket Address"
          type="text"
          value={state.url}
          onChange={({ target: { value } }) => dispatch({ url: value })}
          hasError={state.errors.name !== undefined}
          errorMessage={state.errors.url}
        />
      </div>

      <div css={{ minWidth: MAX_WIDTH, maxWidth: '50%' }}>
        <Textarea
          id="key"
          variant={TEXTAREA_VARIANTS.SECONDARY}
          label="If this bucket is private, please paste the JSON service account key:"
          value={state.key}
          onChange={({ target: { value } }) => dispatch({ key: value })}
          hasError={state.errors.name !== undefined}
          errorMessage={state.errors.key}
        />
      </div>

      <SectionTitle>Select File Types to Import</SectionTitle>

      <SectionTitle>Select Analysis</SectionTitle>

      <SectionSubTitle>
        Choose the type of analysis you would like performed on your data set:
      </SectionSubTitle>

      <DataSourcesAddAutomaticAnalysis />

      <ButtonGroup>
        <Link
          href="/[projectId]/data-sources"
          as={`/${projectId}/data-sources`}
          passHref>
          <Button variant={BUTTON_VARIANTS.SECONDARY}>Cancel</Button>
        </Link>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => console.warn({ dispatch, state })}
          isDisabled={!state.name || !state.url}>
          Create Data Source
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default DataSourcesAddForm
