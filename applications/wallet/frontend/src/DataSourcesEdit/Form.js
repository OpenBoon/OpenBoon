import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'
import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import InformationSvg from '../Icons/information.svg'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import SectionSubTitle from '../SectionSubTitle'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import ButtonGroup from '../Button/Group'
import CheckboxGroup from '../Checkbox/Group'
import Tooltip from '../Tooltip'

import { FILE_TYPES } from '../DataSourcesAdd/helpers'

import DataSourcesAddAutomaticAnalysis from '../DataSourcesAdd/AutomaticAnalysis'

import DataSourcesEditProvider from './Provider'
import DataSourcesEditCopy from './Copy'

import { getInitialModules, onSubmit } from './helpers'

const ICON_SIZE = 20

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
        <DataSourcesEditCopy />

        <div
          css={{
            width: constants.form.maxWidth,
            paddingBottom: spacing.comfy,
          }}
        >
          <SectionTitle>Data Source Name </SectionTitle>

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
          legend="Add Additional File Types"
          subHeader={
            <Tooltip
              content={
                <div
                  css={{
                    color: colors.structure.zinc,
                    padding: spacing.base,
                  }}
                >
                  <h3
                    css={{
                      fontSize: typography.size.regular,
                      lineHeight: typography.height.regular,
                      fontWeight: typography.weight.medium,
                      color: colors.structure.white,
                    }}
                  >
                    Supported File Types
                  </h3>

                  {FILE_TYPES.map(({ value, legend }) => (
                    <div key={value}>
                      <h4
                        css={{
                          paddingTop: spacing.base,
                          fontSize: typography.size.regular,
                          lineHeight: typography.height.regular,
                          fontWeight: typography.weight.bold,
                        }}
                      >
                        {value}:
                      </h4>
                      <h5
                        css={{
                          margin: 0,
                          paddingTop: spacing.mini,
                          fontSize: typography.size.regular,
                          lineHeight: typography.height.regular,
                          fontWeight: typography.weight.regular,
                        }}
                      >
                        {legend}
                      </h5>
                    </div>
                  ))}
                </div>
              }
            >
              <div
                css={{
                  paddingLeft: spacing.base,
                  color: colors.structure.steel,
                  ':hover': {
                    color: colors.structure.white,
                  },
                }}
              >
                <InformationSvg height={ICON_SIZE} />
              </div>
            </Tooltip>
          }
          description="Additional file types can be added to this data source. Previous
              selections cannot be removed."
          onClick={(fileType) =>
            dispatch({ fileTypes: { ...fileTypes, ...fileType } })
          }
          options={FILE_TYPES.map(({ value, label, icon }) => ({
            value,
            label,
            legend:
              value === 'Documents'
                ? 'Pages will be processed and counted as individual assets'
                : '',
            icon: <img src={icon} alt={label} width="40px" />,
            initialValue: !!fileTypes[value],
            isDisabled: !!initialState.fileTypes[value],
          }))}
          variant={CHECKBOX_VARIANTS.INLINE}
        />

        <div css={{ height: spacing.base }} />

        <SectionTitle>Add Additional Analysis</SectionTitle>

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
