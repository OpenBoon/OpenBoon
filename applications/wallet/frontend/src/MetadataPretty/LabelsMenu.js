import PropTypes from 'prop-types'
import { useRouter } from 'next/router'

import { typography } from '../Styles'

import { ACTIONS, dispatch } from '../Filters/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'

const MetadataPrettyLabelsMenu = ({
  label: { datasetId, label, simhash },
  datasetName,
}) => {
  const {
    pathname,
    query: { projectId, id, assetId, query },
  } = useRouter()

  return (
    <Menu open="bottom-left" button={ButtonActions}>
      {({ onBlur, onClick }) => (
        <div css={{ fontFamily: typography.family.regular }}>
          <ul>
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onBlur={onBlur}
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
                        attribute: `labels.${datasetName}`,
                        datasetId,
                        values: { labels: [label] },
                      },
                      query,
                    },
                  })
                }}
              >
                Add Dataset/Label Filter
              </Button>
            </li>

            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onBlur={onBlur}
                onClick={async () => {
                  await navigator.clipboard.writeText(datasetId)

                  onClick()
                }}
              >
                Copy Dataset ID
              </Button>
            </li>

            {simhash && (
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={async () => {
                    await navigator.clipboard.writeText(simhash)

                    onClick()
                  }}
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
    datasetId: PropTypes.string.isRequired,
  }).isRequired,
  datasetName: PropTypes.string.isRequired,
}

export default MetadataPrettyLabelsMenu
