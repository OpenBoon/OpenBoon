import PropTypes from 'prop-types'

import { typography } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'

import AssetLabelingHeader from './Header'
import AssetLabelingAdd from './Add'
import AssetLabelingList from './List'

const AssetLabelingContent = ({ projectId, assetId }) => {
  return (
    <>
      <AssetLabelingHeader projectId={projectId} assetId={assetId} />
      <Accordion
        variant={ACCORDION_VARIANTS.PANEL}
        title={
          <span css={{ display: 'flex' }}>
            Select a model and add a label.{' '}
            <Button
              variant={BUTTON_VARIANTS.LINK}
              href={`/${projectId}/models/add`}
              onClick={(event) => event.stopPropagation()}
              css={{
                paddingTop: 0,
                paddingBottom: 0,
                fontWeight: typography.weight.medium,
              }}
            >
              Create New Model
            </Button>
          </span>
        }
        cacheKey="AssetLabeling.Add"
        isInitiallyOpen
        isResizeable={false}
      >
        <AssetLabelingAdd projectId={projectId} assetId={assetId} />
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
  assetId: PropTypes.string.isRequired,
}

export default AssetLabelingContent
