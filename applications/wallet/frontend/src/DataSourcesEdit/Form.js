import { useReducer } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import SectionSubTitle from '../SectionSubTitle'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import ButtonGroup from '../Button/Group'
import CheckboxGroup from '../Checkbox/Group'

import { FILE_TYPES, MODULES } from '../DataSourcesAdd/helpers'

import DataSourcesAddAutomaticAnalysis from '../DataSourcesAdd/AutomaticAnalysis'

import { onSubmit } from './helpers'

import DataSourcesEditModules from './Modules'

const INITIAL_STATE = {
  fileTypes: {},
  modules: {},
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const DataSourcesEditForm = ({
  projectId,
  dataSource: { id: dataSourceId },
}) => {
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  return (
    <Form style={{ width: 'auto' }}>
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
        <DataSourcesEditModules
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
          onClick={() => onSubmit({ dispatch, projectId, dataSourceId, state })}
          isDisabled={false}>
          Save
        </Button>
      </ButtonGroup>
    </Form>
  )
}

DataSourcesEditForm.propTypes = {
  projectId: PropTypes.string.isRequired,
  dataSource: PropTypes.shape({
    id: PropTypes.string.isRequired,
  }).isRequired,
}

export default DataSourcesEditForm
