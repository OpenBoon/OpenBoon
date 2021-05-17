import PropTypes from 'prop-types'

import Table, { ROLES } from '../Table'

import ModelsRow from './Row'

const ModelsTable = ({ projectId, modelTypes }) => {
  return (
    <Table
      role={ROLES.ML_Tools}
      legend="Models"
      url={`/api/v1/projects/${projectId}/models/`}
      refreshKeys={[]}
      refreshButton={false}
      columns={['Name', 'Type', 'Module', '#Actions#']}
      expandColumn={0}
      renderEmpty="There are currently no custom models."
      renderRow={({ result }) => (
        <ModelsRow
          key={result.id}
          projectId={projectId}
          model={result}
          modelTypes={modelTypes}
        />
      )}
    />
  )
}

ModelsTable.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelTypes: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
}

export default ModelsTable
