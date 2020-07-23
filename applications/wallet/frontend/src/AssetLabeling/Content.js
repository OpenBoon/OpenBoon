import PropTypes from 'prop-types'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import AssetLabelingAdd from './Add'
import AssetLabelingList from './List'

const AssetLabelingContent = ({ projectId }) => {
  return (
    <>
      <Accordion
        variant={ACCORDION_VARIANTS.PANEL}
        title="Select a model and add a label."
        cacheKey="AssetLabeling.Add"
        isInitiallyOpen
        isResizeable={false}
      >
        <AssetLabelingAdd projectId={projectId} />
      </Accordion>
      <Accordion
        variant={ACCORDION_VARIANTS.PANEL}
        title="Asset Labels: 0"
        cacheKey="AssetLabeling.List"
        isInitiallyOpen
        isResizeable={false}
      >
        <AssetLabelingList />
      </Accordion>
    </>
  )
}

AssetLabelingContent.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default AssetLabelingContent
