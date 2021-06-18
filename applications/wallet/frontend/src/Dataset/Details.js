import PropTypes from 'prop-types'
import useSWR from 'swr'

import ItemTitle from '../Item/Title'
import ItemList from '../Item/List'

import { decamelize } from '../Text/helpers'

const DatasetDetails = ({ projectId, datasetId }) => {
  const {
    data: { name, type, description },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/${datasetId}/`)

  return (
    <div>
      <ItemTitle type="Dataset" name={name} />

      <ItemList
        attributes={[
          ['Dataset Type', decamelize({ word: type })],
          ['Description', description],
        ]}
      />
    </div>
  )
}

DatasetDetails.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
}

export default DatasetDetails
