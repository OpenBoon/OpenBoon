import PropTypes from 'prop-types'

import { constants, colors } from '../Styles'

import ModelLinkForm from './Form'

const ModelLink = ({ model }) => {
  return (
    <div>
      <div
        css={{
          maxWidth: constants.paragraph.maxWidth,
          color: colors.structure.zinc,
        }}
      >
        Datasets are groups of labels added to assets that are used by the model
        for training. When using an uploaded, pre-trained model, only the
        testing labels in the dataset are used.
      </div>

      <ModelLinkForm model={model} />
    </div>
  )
}

ModelLink.propTypes = {
  model: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    datasetId: PropTypes.string,
    runningJobId: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired,
    datasetType: PropTypes.string.isRequired,
  }).isRequired,
}

export default ModelLink
