import PropTypes from 'prop-types'
import useSWR from 'swr'

import DatasetsTable from './Table'

const DatasetsContent = ({ projectId }) => {
  const {
    data: { results: datasetTypes },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/dataset_types/`)

  return <DatasetsTable projectId={projectId} datasetTypes={datasetTypes} />
}

DatasetsContent.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default DatasetsContent
