import PropTypes from 'prop-types'
import useSWR from 'swr'

import ModelsTable from './Table'

const ModelsContent = ({ projectId }) => {
  const {
    data: { results: modelTypes },
  } = useSWR(`/api/v1/projects/${projectId}/models/model_types/`)

  return <ModelsTable projectId={projectId} modelTypes={modelTypes} />
}

ModelsContent.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default ModelsContent
