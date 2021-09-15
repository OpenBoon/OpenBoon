import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing } from '../Styles'

import AssetLabelingDataset from '../AssetLabeling/Dataset'

import BulkAssetLabelingContent from './Content'

const BulkAssetLabeling = ({ projectId, query, setIsBulkLabeling }) => {
  const {
    data: { results: datasets },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/all/`)

  return (
    <>
      <div
        css={{
          padding: spacing.base,
          paddingLeft: spacing.normal,
          borderBottom: constants.borders.regular.smoke,
          color: colors.structure.pebble,
        }}
      >
        All Assets in Search
      </div>

      <AssetLabelingDataset
        projectId={projectId}
        assetId=""
        datasets={datasets}
      />

      <BulkAssetLabelingContent
        projectId={projectId}
        query={query}
        setIsBulkLabeling={setIsBulkLabeling}
        datasets={datasets}
      />
    </>
  )
}

BulkAssetLabeling.defaultProps = {
  query: '',
}

BulkAssetLabeling.propTypes = {
  projectId: PropTypes.string.isRequired,
  query: PropTypes.string,
  setIsBulkLabeling: PropTypes.func.isRequired,
}

export default BulkAssetLabeling
