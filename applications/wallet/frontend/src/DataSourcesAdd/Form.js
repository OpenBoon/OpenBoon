import { useReducer } from 'react'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { constants } from '../Styles'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import SectionSubTitle from '../SectionSubTitle'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Textarea, { VARIANTS as TEXTAREA_VARIANTS } from '../Textarea'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import ButtonGroup from '../Button/Group'
import CheckboxGroup from '../Checkbox/Group'

import { FILE_TYPES, MODULES, onSubmit } from './helpers'

import DataSourcesAddAutomaticAnalysis from './AutomaticAnalysis'
import DataSourcesAddModules from './Modules'

const INITIAL_STATE = {
  name: '',
  uri: '',
  credential: '',
  fileTypes: {},
  modules: {},
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
      <div css={{ width: constants.form.maxWidth }}>
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
          id="uri"
          variant={INPUT_VARIANTS.SECONDARY}
          label="Bucket Address"
          type="text"
          value={state.uri}
          onChange={({ target: { value } }) => dispatch({ uri: value })}
          hasError={state.errors.name !== undefined}
          errorMessage={state.errors.uri}
        />
      </div>

      <div css={{ minWidth: constants.form.maxWidth, maxWidth: '50%' }}>
        <Textarea
          id="credential"
          variant={TEXTAREA_VARIANTS.SECONDARY}
          label="If this bucket is private, please paste the JSON service account credential:"
          value={state.credential}
          onChange={({ target: { value } }) => dispatch({ credential: value })}
          hasError={state.errors.name !== undefined}
          errorMessage={state.errors.credential}
        />
      </div>

      <CheckboxGroup
        legend="Select File Types to Import"
        onClick={fileType =>
          dispatch({ fileTypes: { ...state.fileTypes, ...fileType } })
        }
        options={FILE_TYPES.map(({ value, label, legend, icon }) => ({
          value,
          label,
          icon: <img src={icon} alt={label} width="40px" />,
          legend,
          initialValue: false,
          isDisabled: false,
        }))}
        variant={CHECKBOX_VARIANTS.SECONDARY}
      />

      <SectionTitle>Select Analysis</SectionTitle>

      <SectionSubTitle>
        Choose the type of analysis you would like performed on your data set:
      </SectionSubTitle>

      <DataSourcesAddAutomaticAnalysis />

      {MODULES.map(module => (
        <DataSourcesAddModules
          key={module.provider}
          module={module}
          onClick={modules =>
            dispatch({ modules: { ...state.modules, ...modules } })
          }
        />
      ))}

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
          onClick={() => onSubmit({ dispatch, projectId, state })}
          isDisabled={
            !state.name ||
            !state.uri ||
            !Object.values(state.fileTypes).filter(Boolean).length > 0
          }>
          Create Data Source
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default DataSourcesAddForm
