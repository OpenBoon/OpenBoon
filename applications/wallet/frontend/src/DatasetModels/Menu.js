import PropTypes from 'prop-types'
import Link from 'next/link'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import ButtonExternal from '../Button/External'

const DatasetModelsMenu = ({ projectId, modelId }) => {
  return (
    <>
      <Menu open="bottom-left" button={ButtonActions}>
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              <li>
                <Link href={`/${projectId}/models/${modelId}`} passHref>
                  <Button
                    variant={VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={onClick}
                  >
                    <ButtonExternal>View Model</ButtonExternal>
                  </Button>
                </Link>
              </li>
            </ul>
          </div>
        )}
      </Menu>
    </>
  )
}

DatasetModelsMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
}

export default DatasetModelsMenu
