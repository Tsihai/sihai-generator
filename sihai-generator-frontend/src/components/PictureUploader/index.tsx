import {uploadFileUsingPost,} from '@/services/backend/fileController';
import {LoadingOutlined, PlusOutlined} from '@ant-design/icons';
import {message, Upload, UploadProps} from 'antd';
import React, {useState} from 'react';
import {COS_HOST} from "@/constants";
import {values} from "lodash";
import {addGeneratorUsingPost} from "@/services/backend/generatorController";

interface Props {
  biz: string;
  onChange?: (url: string) => void;
  value?: string;
}

/**
 * 图片上传组件
 * @constructor
 */
const PictureUploader: React.FC<Props> = (props) => {

  const {biz, onChange, value} = props;

  /**
   * 判断是否正在上传
   */
  const [loading, setLoading] = useState<boolean>(false);

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: false,
    listType: 'picture-card',
    maxCount: 1,
    disabled: loading,
    showUploadList: false,
    customRequest: async (fileObj: any) => {
      setLoading(true);
      try {
        const res = await uploadFileUsingPost({
          biz
        }, {}, fileObj.file);
        // 拼接完整图片路径
        const fullPath = COS_HOST + res.data;
        onChange?.(fullPath ?? '');
        fileObj.onSuccess(res.data);
      } catch (e: any) {
        message.error('上传失败，' + e.message);
        fileObj.onError(e);
      }
      setLoading(false);
    },
  };

  /**
   * 上传按钮
   */
  const uploadButton = (
    <div>
      {loading ? <LoadingOutlined /> : <PlusOutlined />}
      <div style={{ marginTop: 8 }}>上传</div>
    </div>
  );

  return (
    <Upload {...uploadProps}>
      {value ? <img src={value} alt="picture" style={{ width: '100%' }} /> : uploadButton}
    </Upload>
  );
};

export default PictureUploader;
