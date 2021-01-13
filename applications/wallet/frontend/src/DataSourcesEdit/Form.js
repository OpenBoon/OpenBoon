import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'
import PropTypes from 'prop-types'

import { constants, spacing, typography } from '../Styles'

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

import { FILE_TYPES } from '../DataSourcesAdd/helpers'

import DataSourcesAddAutomaticAnalysis from '../DataSourcesAdd/AutomaticAnalysis'

import DataSourcesEditProvider from './Provider'
import DataSourcesEditCopy from './Copy'

import { getInitialModules, onSubmit } from './helpers'

const reducer = (state, action) => ({ ...state, ...action })

const DataSourcesEditForm = ({ initialState }) => {
  const {
    query: { projectId, dataSourceId },
  } = useRouter()

  const {
    data: { results: providers },
  } = useSWR(`/api/v1/projects/${projectId}/providers/`)

  const initialModules = getInitialModules({
    initialState,
    providers,
  })

  const [state, dispatch] = useReducer(reducer, {
    ...initialState,
    modules: initialModules,
  })

  const { errors, fileTypes, name, uri } = state

  return (
    <>
      <FlashMessageErrors
        errors={errors}
        styles={{ paddingTop: spacing.base, paddingBottom: spacing.base }}
      />

      <Form style={{ width: 'auto' }}>
        <DataSourcesEditCopy />

        <div
          css={{
            width: constants.form.maxWidth,
            paddingBottom: spacing.comfy,
          }}
        >
          <SectionTitle>STEP 1: Data Source Name </SectionTitle>

          <Input
            autoFocus
            id="name"
            variant={INPUT_VARIANTS.SECONDARY}
            label="Name"
            type="text"
            value={name}
            onChange={({ target: { value } }) =>
              dispatch({
                name: value,
              })
            }
            hasError={!!errors.name || !name}
            errorMessage={errors.name || (!name ? 'Name cannot be empty' : '')}
          />

          <SectionTitle>{`Storage Address: ${uri}`}</SectionTitle>
        </div>

        <CheckboxGroup
          legend={
            <>
              STEP 2: Add Additional File Types
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
            </>
          }
          description={
            <div>
              Additional file types can be added to this data source. Previous
              selections cannot be removed.
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
            initialValue: !!fileTypes[value],
            isDisabled: !!initialState.fileTypes[value],
          }))}
          variant={CHECKBOX_VARIANTS.SECONDARY}
        />

        <div css={{ height: spacing.base }} />

        <SectionTitle>STEP 3: Add Additional Analysis</SectionTitle>

        <SectionSubTitle>
          Additional analysis can be added to this data source. Previous
          selections cannot be removed.
        </SectionSubTitle>

        <DataSourcesAddAutomaticAnalysis />

        {providers.map((provider) => (
          <DataSourcesEditProvider
            key={provider.name}
            provider={provider}
            initialModules={initialModules}
            modules={state.modules}
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
            onClick={() =>
              onSubmit({ dispatch, projectId, dataSourceId, state })
            }
            isDisabled={!name || state.isLoading}
          >
            {state.isLoading ? 'Updating...' : 'Update Data Source'}
          </Button>
        </ButtonGroup>
      </Form>
    </>
  )
}

DataSourcesEditForm.propTypes = {
  initialState: PropTypes.shape({
    name: PropTypes.string.isRequired,
    uri: PropTypes.string.isRequired,
    fileTypes: PropTypes.shape({
      Images: PropTypes.bool,
      Videos: PropTypes.bool,
      Documents: PropTypes.bool,
    }).isRequired,
    modules: PropTypes.arrayOf(PropTypes.string).isRequired,
    errors: PropTypes.shape({ global: PropTypes.string }).isRequired,
  }).isRequired,
}

export default DataSourcesEditForm
