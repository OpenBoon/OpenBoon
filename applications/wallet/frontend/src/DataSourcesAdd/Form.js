import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'

import { colors, constants, typography, spacing } from '../Styles'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import SectionSubTitle from '../SectionSubTitle'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import FlashMessageErrors from '../FlashMessage/Errors'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import ButtonGroup from '../Button/Group'
import CheckboxGroup from '../Checkbox/Group'
import Toggletip from '../Toggletip'

import { FILE_TYPES, onSubmit } from './helpers'

import DataSourcesAddAutomaticAnalysis from './AutomaticAnalysis'
import DataSourcesAddProvider from './Provider'
import DataSourcesAddCopy from './Copy'
import DataSourcesAddSource, { SOURCES } from './Source'

const INITIAL_STATE = {
  name: '',
  source: '',
  uri: '',
  credentials: {},
  fileTypes: { Documents: true, Images: true, Videos: true },
  modules: {},
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const DataSourcesAddForm = () => {
  const {
    query: { projectId },
  } = useRouter()

  const {
    data: { results: providers },
  } = useSWR(`/api/v1/projects/${projectId}/providers/`)

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  const { credentials, errors, fileTypes, name, source, uri } = state

  const isFileTypesEmpty = !Object.values(fileTypes).find((value) => !!value)

  const isRequiredCredentialsEmpty = credentials[source]
    ? Object.keys(credentials[source]).reduce((count, credential) => {
        const { isRequired, value } = credentials[source][credential]
        const currentCount = isRequired && value === '' ? 1 : 0
        return count + currentCount
      }, 0) > 0
    : true

  return (
    <>
      <FlashMessageErrors
        errors={errors}
        styles={{ paddingTop: spacing.base, paddingBottom: spacing.base }}
      />
      <Form style={{ width: 'auto' }}>
        <DataSourcesAddCopy />

        <div css={{ width: constants.form.maxWidth }}>
          <span
            css={{
              fontStyle: typography.style.italic,
              color: colors.structure.zinc,
            }}
          >
            <span css={{ color: colors.signal.warning.base }}>*</span> required
            field
          </span>

          <SectionTitle>STEP 1: Data Source Name </SectionTitle>

          <Input
            autoFocus
            id="name"
            variant={INPUT_VARIANTS.SECONDARY}
            label="Name"
            type="text"
            value={name}
            onChange={({ target: { value } }) => {
              const nameError = value === '' ? 'Name cannot be empty' : ''

              return dispatch({
                name: value,
                errors: { ...errors, name: nameError },
              })
            }}
            hasError={!!errors.name}
            errorMessage={errors.name}
            isRequired
          />

          <SectionTitle>STEP 2: Connect to Source</SectionTitle>

          <DataSourcesAddSource dispatch={dispatch} state={state} />
        </div>

        <CheckboxGroup
          legend={
            <div css={{ display: 'flex' }}>
              STEP 3: Determine Import File Type(s)
              <Toggletip openToThe="right" label="Supported File Types">
                <div
                  css={{
                    fontSize: typography.size.regular,
                    lineHeight: typography.height.regular,
                  }}
                >
                  <h3
                    css={{
                      fontSize: typography.size.regular,
                      lineHeight: typography.height.regular,
                      paddingBottom: spacing.base,
                    }}
                  >
                    Supported File Types
                  </h3>
                  {FILE_TYPES.map(({ value, extensions }) => (
                    <div key={value} css={{ paddingBottom: spacing.base }}>
                      <h4>{value}:</h4>
                      {extensions}
                    </div>
                  ))}
                </div>
              </Toggletip>
            </div>
          }
          description={
            <div>
              You can import all supported file types or limit them by editing
              the selection below.
              <br />A minimum of one file type must be selected.{' '}
              <span css={{ color: colors.signal.warning.base }}>*</span>
            </div>
          }
          onClick={(fileType) =>
            dispatch({ fileTypes: { ...fileTypes, ...fileType } })
          }
          options={FILE_TYPES.map(({ value, label, legend, icon }) => ({
            value,
            label,
            icon,
            legend,
            initialValue: state.fileTypes[value],
            isDisabled: false,
          }))}
          variant={CHECKBOX_VARIANTS.SECONDARY}
        />

        <div css={{ height: spacing.base }} />

        <SectionTitle>STEP 4: Select Analysis</SectionTitle>

        <SectionSubTitle>
          Choose the type of analysis you’d like performed on your dataset.
          <br />
          (Only modules that can be applied to the file type selected above will
          be shown.)
        </SectionSubTitle>

        <DataSourcesAddAutomaticAnalysis />

        {providers.map((provider) => (
          <DataSourcesAddProvider
            key={provider.name}
            provider={provider}
            onClick={(module) =>
              dispatch({ modules: { ...state.modules, ...module } })
            }
          />
        ))}

        <ButtonGroup>
          <Link
            href="/[projectId]/data-sources"
            as={`/${projectId}/data-sources`}
            passHref
          >
            <Button variant={BUTTON_VARIANTS.SECONDARY}>Cancel</Button>
          </Link>
          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() => onSubmit({ dispatch, projectId, state })}
            isDisabled={
              !name ||
              !source ||
              uri === SOURCES[source].uri ||
              !!errors.uri ||
              isRequiredCredentialsEmpty ||
              isFileTypesEmpty ||
              state.isLoading
            }
          >
            {state.isLoading ? 'Creating...' : 'Create Data Source'}
          </Button>
        </ButtonGroup>
      </Form>
    </>
  )
}

export default DataSourcesAddForm
