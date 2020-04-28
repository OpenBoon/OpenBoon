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
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import ButtonGroup from '../Button/Group'
import CheckboxGroup from '../Checkbox/Group'

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
  fileTypes: {},
  modules: {},
  errors: { global: '', name: '', uri: '' },
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
      {errors.global && (
        <div
          css={{
            display: 'flex',
            paddingTop: spacing.base,
            marginBottom: -spacing.base,
          }}
        >
          <FlashMessage variant={FLASH_VARIANTS.ERROR}>
            {errors.global}
          </FlashMessage>
        </div>
      )}

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

          <SectionTitle>Data Source Name </SectionTitle>

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

          <SectionTitle>Connect to Source</SectionTitle>

          <DataSourcesAddSource dispatch={dispatch} state={state} />
        </div>

        <div css={{ paddingBottom: spacing.base }}>
          <CheckboxGroup
            legend="Select File Types to Import"
            description={
              <div>
                A minimum of one file type must be selected{' '}
                <span css={{ color: colors.signal.warning.base }}>*</span>
              </div>
            }
            onClick={(fileType) =>
              dispatch({ fileTypes: { ...fileTypes, ...fileType } })
            }
            options={FILE_TYPES.map(({ value, label, legend, icon }) => ({
              value,
              label,
              icon: <img src={icon} alt={label} width="40px" />,
              legend,
              initialValue: false,
              isDisabled: false,
            }))}
            variant={CHECKBOX_VARIANTS.INLINE}
          />
        </div>

        <SectionTitle>Select Analysis</SectionTitle>
        <SectionSubTitle>
          Choose the type of analysis you would like performed on your data set:
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
              isFileTypesEmpty
            }
          >
            Create Data Source
          </Button>
        </ButtonGroup>
      </Form>
    </>
  )
}

export default DataSourcesAddForm
