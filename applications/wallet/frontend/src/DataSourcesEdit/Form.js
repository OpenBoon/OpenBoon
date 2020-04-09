import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'
import PropTypes from 'prop-types'

import { colors, constants, typography } from '../Styles'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import SectionSubTitle from '../SectionSubTitle'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Textarea, { VARIANTS as TEXTAREA_VARIANTS } from '../Textarea'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import ButtonGroup from '../Button/Group'
import CheckboxGroup from '../Checkbox/Group'

import { FILE_TYPES } from '../DataSourcesAdd/helpers'

import { onSubmit } from './helpers'

import DataSourcesAddAutomaticAnalysis from '../DataSourcesAdd/AutomaticAnalysis'
import DataSourcesAddProvider from '../DataSourcesAdd/Provider'

const reducer = (state, action) => ({ ...state, ...action })

const noop = () => () => {}

const DataSourcesEditForm = ({ initialState }) => {
  const {
    query: { projectId, dataSourceId },
  } = useRouter()

  const {
    data: { results: providers },
  } = useSWR(`/api/v1/projects/${projectId}/providers/`)

  const [state, dispatch] = useReducer(reducer, initialState)

  console.log(state)

  return (
    <>
      {state.errors.global && (
        <FlashMessage variant={FLASH_VARIANTS.ERROR}>
          {state.errors.global}
        </FlashMessage>
      )}

      <Form style={{ width: 'auto' }}>
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
            value={state.name}
            onChange={({ target: { value } }) => dispatch({ name: value })}
            hasError={state.errors.name !== undefined}
            errorMessage={state.errors.name}
            isRequired
          />

          <SectionTitle>Connect to Source: Google Cloud Storage</SectionTitle>

          <Input
            id="uri"
            variant={INPUT_VARIANTS.SECONDARY}
            label="Bucket Address"
            type="text"
            value={state.uri}
            onChange={noop}
            hasError={false}
            errorMessage=""
            isRequired
            isDisabled
          />
        </div>

        <div css={{ minWidth: constants.form.maxWidth, maxWidth: '50%' }}>
          <Textarea
            id="credentials"
            variant={TEXTAREA_VARIANTS.SECONDARY}
            label="If this bucket is private, please paste the JSON service account credentials:"
            value={state.credentials}
            onChange={({ target: { value } }) =>
              dispatch({ credentials: value })
            }
            hasError={state.errors.credentials !== undefined}
            errorMessage={state.errors.credentials}
          />
        </div>

        <CheckboxGroup
          legend="Select File Types to Import"
          description={
            <div>
              A minimum of one file type must be selected{' '}
              <span css={{ color: colors.signal.warning.base }}>*</span>
            </div>
          }
          onClick={(fileType) =>
            dispatch({ fileTypes: { ...state.fileTypes, ...fileType } })
          }
          options={FILE_TYPES.map(({ value, label, legend, icon }) => ({
            value,
            label,
            icon: <img src={icon} alt={label} width="40px" />,
            legend,
            initialValue: state.fileTypes[value] || false,
            isDisabled: false,
          }))}
          variant={CHECKBOX_VARIANTS.SECONDARY}
        />

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
              dispatch({ modules: { ...state.module, ...module } })
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
            onClick={() =>
              onSubmit({ dispatch, projectId, dataSourceId, state })
            }
            isDisabled={
              !state.name ||
              state.uri.substr(0, 5) !== 'gs://' ||
              !Object.values(state.fileTypes).filter(Boolean).length > 0
            }
          >
            Edit Data Source
          </Button>
        </ButtonGroup>
      </Form>
    </>
  )
}

DataSourcesEditForm.propTypes = {
  initialState: PropTypes.shape({
    name: PropTypes.string,
    uri: PropTypes.string,
    credentials: PropTypes.array,
    fileTypes: PropTypes.object,
    modules: PropTypes.arrayOf(PropTypes.string),
    errors: PropTypes.shape({ global: PropTypes.string }),
  }).isRequired,
}

export default DataSourcesEditForm
