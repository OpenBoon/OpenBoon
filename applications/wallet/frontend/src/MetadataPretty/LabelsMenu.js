import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useClipboard from 'react-use-clipboard'

import { typography } from '../Styles'

import { ACTIONS, dispatch } from '../Filters/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'

const MetadataPrettyLabelsMenu = ({
  label: { modelId, label, simhash },
  moduleName,
}) => {
  const {
    pathname,
    query: { projectId, id, assetId, query },
  } = useRouter()

  const [, copySimhash] = useClipboard(simhash)
  const [, copyModelId] = useClipboard(modelId)

  return (
    <Menu open="left" button={ButtonActions}>
      {({ onClick }) => (
        <div css={{ fontFamily: typography.family.regular }}>
          <ul>
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onClick={() => {
                  onClick()

                  dispatch({
                    type: ACTIONS.ADD_VALUE,
                    payload: {
                      pathname,
                      projectId,
                      assetId: id || assetId,
                      filter: {
                        type: 'label',
                        attribute: `labels.${moduleName}`,
                        modelId,
                        values: { labels: [label] },
                      },
                      query,
                    },
                  })
                }}
                isDisabled={false}
              >
                Add Model/Label Filter
              </Button>
            </li>
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onClick={() => {
                  copyModelId()
                  onClick()
                }}
                isDisabled={false}
              >
                Copy Model ID
              </Button>
            </li>
            {simhash && (
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onClick={() => {
                    copySimhash()
                    onClick()
                  }}
                  isDisabled={false}
                >
                  Copy Simhash
                </Button>
              </li>
            )}
          </ul>
        </div>
      )}
    </Menu>
  )
}

MetadataPrettyLabelsMenu.propTypes = {
  label: PropTypes.shape({
    label: PropTypes.string.isRequired,
    simhash: PropTypes.string,
    modelId: PropTypes.string.isRequired,
  }).isRequired,
  moduleName: PropTypes.string.isRequired,
}

export default MetadataPrettyLabelsMenu
